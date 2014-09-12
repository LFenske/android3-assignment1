package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.magnum.dataup.VideoSvcApi;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
//import org.magnum.dataup.model.VideoFileManager;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.boot.context.embedded.MultiPartConfigFactory;

import javax.servlet.MultipartConfigElement;

//import retrofit.client.Response;
//import retrofit.http.Body;
//import retrofit.http.GET;
//import retrofit.http.POST;
//import retrofit.http.Part;
//import retrofit.http.Path;
//import retrofit.mime.TypedFile;

@Controller
public class VideoController {
	
    private static final AtomicLong currentId = new AtomicLong(0L);
	
	private Map<Long,Video> videos = new HashMap<Long, Video>();

  	public Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		entity.setDataUrl(getDataUrl(entity.getId()));
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}


    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}

 	
 	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videos.values();
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		return save(v);
	}

	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable("id") long id,
			@RequestParam("data") MultipartFile videoData,
    		HttpServletResponse response) throws IOException {
		InputStream in = videoData.getInputStream();
		Video v = videos.get(id);
		if (v == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
		} else {
			VideoFileManager vfm = VideoFileManager.get();
			vfm.saveVideoData(v, in);
		}
		return new VideoStatus(VideoStatus.VideoState.READY);
	}

    @RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.GET)
    public void getData(
    		@PathVariable("id") long id,
    		HttpServletResponse response) throws IOException {
    	
    	Video v = videos.get(id);
    	if (v == null) {
        	response.setStatus(HttpStatus.NOT_FOUND.value());    		
    	} else {
    		VideoFileManager vfm = VideoFileManager.get();
    		response.addHeader("Content-Type", v.getContentType());
    		vfm.copyVideoData(v, response.getOutputStream());
    	}
		return;
    }
    
    @Bean
    public MultipartConfigElement multipartConfig() {
    	MultiPartConfigFactory f = new MultiPartConfigFactory();
    	f.setMaxFileSize(2000000);
    	f.setMaxRequestSize(2000000);
    	return f.createMultipartConfig();
    }

	
}
