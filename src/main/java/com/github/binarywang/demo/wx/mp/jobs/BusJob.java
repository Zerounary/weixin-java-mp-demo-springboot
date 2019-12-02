package com.github.binarywang.demo.wx.mp.jobs;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONTools;
import com.bus.shanghai.BusQueryUtils;

import lombok.AllArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;

@Component
@AllArgsConstructor
@EnableScheduling
public class BusJob {
	
    private final WxMpService wxService;
    
	@Scheduled(cron = "00 0/1 * * * ?")
	private void configureTasks() {
		System.out.println("上海公交车定时任务启动");
		JSONObject sessions;
		try {
			sessions = JSONTools.inObject("sessions.json");
			Set<String> users = sessions.keySet();
			JSONObject user = null;
			for (String userid : users) {
				user = sessions.getJSONObject(userid);
				if(user.getBoolean("isOrder")) {
					System.out.println("推送用户：" + userid);
					int stoptype = user.getIntValue("direction");
					JSONObject rst = BusQueryUtils.queryFx15(stoptype);
					StringBuffer content = new StringBuffer();
					content.append("方向：" + (stoptype==0? "前往【西渡】" : "远离【西渡】"));
					content.append("\r\n");
					int time = rst.getIntValue("time");
					content.append("时间：" + (time/60) + "分" + (time%60) + "秒");
					content.append("\r\n");
					Date expDate = new Date(System.currentTimeMillis() + (time * 1000));
					content.append("预计到达时间：" + expDate.getHours() + ":" + expDate.getMinutes());
					WxMpKefuMessage msg = 
			        		WxMpKefuMessage
			        			.TEXT()
			        			.toUser(userid)
			        			.content(content.toString())
			        			.build();
			        try {
						wxService.getKefuService().sendKefuMessage(msg);
					} catch (WxErrorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
