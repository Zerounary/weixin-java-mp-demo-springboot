package com.github.binarywang.demo.wx.mp.handler;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONTools;
import com.bus.shanghai.BusQueryUtils;
import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.utils.JSONRepl;
import com.github.binarywang.demo.wx.mp.utils.JsonUtils;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType;

/**
 * @author Binary Wang(https://github.com/binarywang)
 */
@Component
public class MsgHandler extends AbstractHandler {
	

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context, WxMpService weixinService,
                                    WxSessionManager sessionManager) {

        if (!wxMessage.getMsgType().equals(XmlMsgType.EVENT)) {
            //TODO 可以选择将消息保存到本地
        }

        //当用户输入关键词如“你好”，“客服”等，并且有客服在线时，把消息转发给在线客服
        try {
            if (StringUtils.startsWithAny(wxMessage.getContent(), "你好", "客服")
                && weixinService.getKefuService().kfOnlineList()
                .getKfOnlineList().size() > 0) {
                return WxMpXmlOutMessage.TRANSFER_CUSTOMER_SERVICE()
                    .fromUser(wxMessage.getToUser())
                    .toUser(wxMessage.getFromUser()).build();
            }
        } catch (WxErrorException e) {
            e.printStackTrace();
        }

        //TODO 组装回复消息
//        String content = "收到信息内容：" + JsonUtils.toJson(wxMessage);
//        String content = BusRepl.repl(wxMessage.getFromUser(),wxMessage.getContent(), weixinService);
        String content = JCRepl.repl(wxMessage.getFromUser(),wxMessage.getContent(), weixinService);
        return new TextBuilder().build(content, wxMessage, weixinService);

    }

}

class JCRepl{
	private static JSONRepl jsonRepl = new JSONRepl();
	public static String repl(String session, String query, WxMpService weixinService) {
		try {
			return jsonRepl.repl(session, query);
		}catch (Exception e) {
			// TODO: handle exception
			return e.getMessage();
		}
	} 
}

class BusRepl{

	private static JSONObject sessionManager = new JSONObject();
	static {
		try {
			sessionManager = JSONTools.inObject("sessions.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String repl(String session, String query, WxMpService weixinService) {
		String rep = "";
		JSONObject targetSession = sessionManager.getJSONObject(session);
		if(targetSession == null) {
			sessionManager.put(session, new JSONObject());
			sessionManager.getJSONObject(session).put("direction", 0);
			return "哈罗，你可以跟我预定15路公交站。后车快来的时候，我发消息告诉你喔 ( ^_^ )，要预定吗？";
		} 
		
		if(isHelp(query)) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("跟我说：“预定”，可以启动消息通知");
			buffer.append("\r\n");;
			buffer.append("跟我说：“取消预定”，可以暂停消息通知");
			buffer.append("\r\n");;
			buffer.append("跟我说：“调换方向”，可以调换路线方向");
			buffer.append("\r\n");;
//			buffer.append("跟我说：“选择线路”，可以调换路线方向");
			
			rep = buffer.toString();
		}
		else if(isOrderStation(query)) {
			targetSession.put("isOrder", true);
			
			rep = "预定成功！";
		}else if(isCancelOrder(query)) {
			targetSession.put("isOrder", false);
			rep = "取消预定成功！";
		}else if(setDirection(query)) {
			int direction = targetSession.getInteger("direction");
			targetSession.put("direction", direction==0?1:0);
			rep = "方向已调换";
		}
		
		try {
			JSONTools.outObject("sessions.json", sessionManager);
			return rep;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "会话存储异常";
		}
	}


	private static boolean isHelp(String query) {
		// TODO Auto-generated method stub
		if(query.matches(".*会做什么.*"))
			return true;
		if(query.matches(".*会什么.*"))
			return true;
		return false;
	}


	private static boolean setDirection(String query) {
		// TODO Auto-generated method stub
		if(query.equals("调换方向"))
			return true;
		return false;
	}

	private static boolean isCancelOrder(String query) {
		// TODO Auto-generated method stub
		if(query.equals("取消预定"))
			return true;
		return false;
	}

	private static boolean isOrderStation(String query) {
		// TODO Auto-generated method stub
		if(query.equals("预定"))
			return true;
		return false;
	}
}
