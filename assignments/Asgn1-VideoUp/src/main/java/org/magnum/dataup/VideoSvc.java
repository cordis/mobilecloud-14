/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class VideoSvc
{
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    class VideoNotFoundException extends RuntimeException {}

    private AtomicLong sequence = new AtomicLong(0);

    private HashMap<Long, Video> videoDict = new HashMap<>();

    @RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
    public @ResponseBody Collection<Video> getVideoList()
    {
        return videoDict.values();
    }

    @RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
    public @ResponseBody Video addVideo(@RequestBody Video video)
    {
        long videoId = generateId();
        video.setId(videoId);
        video.setDataUrl(generateUrl(videoId));
        videoDict.put(videoId, video);
        return video;
    }

    private long generateId()
    {
        return sequence.incrementAndGet();
    }

    private String generateUrl(long videoId)
    {
        return getUrlBaseForLocalServer() + VideoSvcApi.VIDEO_DATA_PATH.replace("{id}", Long.toString(videoId));
    }

    private String getUrlBaseForLocalServer()
    {
        ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String baseHref = "";
        baseHref += request.getScheme() + "://";
        baseHref += request.getServerName();
        if (80 != request.getServerPort()) {
            baseHref += ":" + request.getServerPort();
        }
        return baseHref;
    }

    @RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
    public @ResponseBody VideoStatus setVideoData(
            @PathVariable(VideoSvcApi.ID_PARAMETER) long videoId,
            @RequestParam(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData
        ) throws IOException, VideoNotFoundException
    {
        VideoFileManager.get().saveVideoData(getVideoById(videoId), videoData.getInputStream());
        return new VideoStatus(VideoStatus.VideoState.READY);
    }

    @RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
    public void getData(
            @PathVariable(VideoSvcApi.ID_PARAMETER) long videoId,
            HttpServletResponse response
        ) throws IOException, VideoNotFoundException
    {
        Video video = getVideoById(videoId);
        response.setContentType(video.getContentType());
        VideoFileManager.get().copyVideoData(video, response.getOutputStream());
    }

    private Video getVideoById(long videoId)
    {
        Video video = videoDict.get(videoId);
        if (null == video) {
            throw new VideoNotFoundException();
        }
        return video;
    }

}
