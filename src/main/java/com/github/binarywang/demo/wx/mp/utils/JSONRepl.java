package com.github.binarywang.demo.wx.mp.utils;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.alibaba.fastjson.JSONTools;

import java.io.IOException;


public class JSONRepl {
    private JSONObject sessionManager;
    private String fileName = "JsonReplSessions";
    private String path;
    private Object rootObject;
    private Object rstObject;

    public JSONRepl(){
        init();
    }
    public JSONRepl(Object rootObject){
        this.rootObject = rootObject;
        init();
    }
    private void init (){
        this.path = "$";
        try {
            sessionManager = JSONTools.inObject(fileName + ".json");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            sessionManager = new JSONObject();
        }
    }

    /***
     * --------------------------------------------------------
     *                      语义理解
     *
     *   eval：          对Path求值
     *   set：           对Path赋值
     *   arrayAdd：      对Path指向的数组添加值
     *   containsValue： Path指向的目标是否包含某值
     *   keySet:        当前Path指向目标对象的所有属性名
     *   size：          当前Path指向目标对象的非空属性长度，或数组大小
     * --------------------------------------------------------
     */
    public String repl(String session, String query){
        JSONObject targetSession = sessionManager.getJSONObject(session);
        if(targetSession == null){
            sessionManager.put(session, new JSONObject());
            return "哈罗，你可以通过跟我对话来操控一JSON";
        }else{
            if(this.rootObject == null){
                if(targetSession.get("rootObject") == null){
                    targetSession.put("rootObject", new JSONObject());
                }
                this.rootObject = targetSession.get("rootObject");
            }
        }
        String rep = "";
        if(isHelp(query)) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("【跟我说】：“查看[Path路径]”，可以查看对应的值，如果是对象或者数组会切换过去");
            buffer.append("\r\n");;
            buffer.append("【跟我说】：“设置(对象|列表)? [Path路径] [值]”，可以创建属性");
            buffer.append("\r\n");;
            buffer.append("【跟我说】：“添加(对象|列表)? [值]”，只有当前指向的是数组时刻操作添加值， 对象和列表不需要赋值");
            buffer.append("\r\n");;
            buffer.append("【跟我说】：“查看所有属性名”，查看当前对象的所有属性名");
            buffer.append("\r\n");;
            buffer.append("【跟我说】：“查看所有属性值”，查看当前目标的所有属性值");
            buffer.append("\r\n");;
            buffer.append("【跟我说】：“数量”，查看当前目标的属性数量");
            buffer.append("\r\n");;
            buffer.append("【跟我说】：“[Path路径] [值]存在吗”，查看当前对象的所有属性名");
            buffer.append("\r\n");;
            buffer.append("【跟我说】：“删除 [Path路径]”，删除属性");
            buffer.append("\r\n");;
            buffer.append("【跟我说】：“回归根目录”，回归到根目录");
            buffer.append("\r\n");;
            buffer.append("【跟我说】：“回归上级”，回归到上一级根目录");
            buffer.append("\r\n");;
            rep = buffer.toString();
        }
        else if(isJSON(query)) {
            this.set(JSONObject.parse(query));
            rep = "已将该JSON替换" + this.path;
        }
        else if(isMovePath(query)) {
        	if("回归根目录".equals(query.trim())){
            	this.setPathToRoot();
            	rep = "已回归根目录";
        	}else if("回归上级".equals(query.trim())) {
        		this.setPathToParent();
            	rep = "已回归上级目录" + this.path;
        	}
        }
        else if(isEval(query)){
            String oldPath = this.path;
            String newPath = parseQueryPath(query);
            Object targetObject = JSONPath.eval(this.rootObject, newPath);
            if( targetObject instanceof JSONArray || targetObject instanceof JSONObject){
                this.setPath(newPath);
                rep = "切换到" + newPath;
            }else{
                Object val = JSONPath.eval(this.rootObject, newPath);
                if(val != null)
                    rep = val.toString();
                else
                    rep = newPath + ",不存在";
            }
        }else if(isSet(query)){
            String newPath = parseQueryPath(query);
            Object targetObject = JSONPath.eval(this.rootObject, newPath);
            if(query.startsWith("设置对象")){
                JSONPath.set(this.rootObject, newPath, new JSONObject());
            }else if(query.startsWith("设置列表")){
                JSONPath.set(this.rootObject, newPath, new JSONArray());
            }else{
                JSONPath.set(this.rootObject, newPath, parseQueryValue(query));
            }
            rep =  "设置成功";
        }else if(isArrayAdd(query)){
            Object val = this.eval();
            if(val instanceof JSONArray){
                if(query.startsWith("添加对象")){
                    this.arrayAdd(new JSONObject());
                }else if(query.startsWith("添加列表")){
                    this.arrayAdd(new JSONArray());
                }else{
                    this.arrayAdd(parseQueryValue(query));
                }
                rep =  "添加成功";
            }else{
                rep =  "Path当前指向的类型不是数组";
            }
        }else if(isRemove(query)){
            String newPath = parseQueryPath(query);
            this.remove(newPath);
            rep = "删除成功";
        }else if(isContainsValue(query)){
            String newPath = parseQueryPath(query);
            Object val = parseQueryValue(query);
            if(JSONPath.containsValue(this.rootObject, newPath, val)){
                rep = "存在";
            }else {
                rep = "不存在";
            };
        }else if(isKeySet(query)){
            rep = StringUtils.join(this.keySet(),",");
        }else if(isKeyVal(query)){
            rep = JSONPath.eval(this.rootObject, this.path + "[*]").toString();
        }else if(isSize(query)){
            rep = Integer.toString(this.size());
        }else {
            return "query不匹配任一操作类型";
        }
        try {
            targetSession.put("rootObject", this.rootObject);
            JSONTools.outObject(fileName + ".json", sessionManager);
            return rep;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return "会话存储异常";
        }
    }
    
    private boolean isJSON(String query) {
		// TODO Auto-generated method stub
		return JSONObject.isValid(query);
	}
	private boolean isMovePath(String query) {
        if(query.startsWith("回归"))
            return true;
        return false;
    }
    
    private boolean isKeyVal(String query) {
        if(query.endsWith("所有属性值"))
            return true;
        return false;
    }

    private boolean isRemove(String query) {
        if(query.startsWith("删除"))
            return true;
        return false;
    }


    private boolean isHelp(String query) {
        if(query.matches(".*会做什么.*"))
            return true;
        if(query.matches(".*会什么.*"))
            return true;
        return false;
    }

    private boolean isSize(String query) {
        if(query.endsWith("数量"))
            return true;
        return false;
    }

    private boolean isKeySet(String query) {
        if(query.endsWith("所有属性名"))
            return true;
        return false;
    }

    private boolean isContainsValue(String query) {
        if(query.endsWith("存在吗"))
            return true;
        return false;
    }

    private boolean isArrayAdd(String query) {
        if(query.startsWith("添加"))
            return true;
        return false;
    }

    private boolean isSet(String query) {
        if(query.startsWith("设置"))
            return true;
        return false;
    }

    private boolean isEval(String query) {
        if(query.startsWith("查看") && !(isKeySet(query) || isKeyVal(query))){
            return true;
        }
        return false;
    }
    /**
     * 从query中提取各功能的Path参数
     * @param query
     * @return
     */
    private String parseQueryPath(String query) {
        if(isEval(query)){
        	String tgp = StringUtils.removeFirst(query, "查看").trim();
        	if(tgp.startsWith("["))
        		return this.path + tgp;
            return this.path + "." + tgp;
        }else if(isSet(query)){
            return this.path + "." + StringUtils.substringBeforeLast(StringUtils.removeFirst(query, "设置(对象|列表)?").trim(), " ");
        }else if(isContainsValue(query)){
            return this.path + "." + StringUtils.substringBeforeLast(query, " ");
        }else if(isRemove(query)){
            return this.path + "." + StringUtils.substringAfterLast(query, " ");
        }
        return "$";
    }

    /**
     * 从query中提取各功能的值参数
     * @param query
     * @return
     */
    private Object parseQueryValue(String query) {
        if(isSet(query)){
            String valStr = StringUtils.substringAfterLast(StringUtils.removeFirst(query, "设置(对象|列表)?").trim(), " ");
            return StringUtils.isNumeric(valStr) ? (StringUtils.contains(valStr, '.')? Double.parseDouble(valStr) : Long.parseLong(valStr)): valStr;
        }else if(isArrayAdd(query)){
            String valStr = StringUtils.removeFirst(query, "添加(对象|列表)?").trim();
            boolean testS = StringUtils.contains(valStr, ".");
            return valStr.matches("^(-?\\d+)(\\.\\d+)?$") ? (StringUtils.contains(valStr, ".")?  Double.parseDouble(valStr): Long.parseLong(valStr)): valStr;
        }else if(isRemove(query)){
            String valStr = StringUtils.substringAfterLast(query, " ").trim();
            return valStr;
        }else if(isContainsValue(query)){
            String valStr = StringUtils.substringAfterLast(query, " ").trim();
            return StringUtils.removeEnd(valStr, "存在吗");
        }
        return null;
    }
    /***
     * --------------------------------------------------------
     *                      操作类型
     *
     *   eval：          对Path求值
     *   set：           对Path赋值
     *   arrayAdd：      对Path指向的数组添加值
     *   containsValue： Path指向的目标是否包含某值
     *   keySet:        当前Path指向目标对象的所有属性名
     *   size：          当前Path指向目标对象的非空属性长度，或数组大小
     * --------------------------------------------------------
     */
    /**
     * 对Path求值
     * @return
     */
    private Object eval(){
        return JSONPath.eval(this.rootObject, this.path);
    }

    /**
     * 对Path赋值, 传回修改前后的值，方便以后撤销操作
     * 属性不存在会新建，连续不存在的也会自动将父属性设置为对象，子属性设置为该对象属性
     * @param newValue
     * @return
     */
    private Object set(Object newValue){
        Object oldValue = this.eval();
        JSONObject rst = new JSONObject();
        if(JSONPath.set(this.rootObject, this.path, newValue)){
            rst.put("isSuccess", true);
            rst.put("path", this.path);
            rst.put("oldValue", oldValue);
            rst.put("newValue", newValue);
        }else{
            rst.put("isSuccess", false);
            rst.put("path", this.path);
        };
        return rst;
    }

    /**
     *
     * @param value
     * @return
     */
    private Object arrayAdd(Object value){
        JSONObject rst = new JSONObject();
        JSONPath.arrayAdd(this.rootObject, this.path, value);
        rst.put("isSuccess", true);
        rst.put("path", this.path + "[" + (this.size()-1) + "]");
        rst.put("oldValue", null);
        rst.put("newValue", value);
        return rst;
    }
    private Object remove(String removePath){
        JSONObject rst = new JSONObject();
        Object oldValue = JSONPath.eval(this.rootObject, removePath);
        if(JSONPath.remove(this.rootObject, removePath)){
            rst.put("isSuccess", true);
            rst.put("path", removePath);
            rst.put("oldValue", oldValue);
            rst.put("newValue", null);
        }else{
            rst.put("isSuccess", false);
            rst.put("path", removePath);
        }
        return rst;
    }
    /**
     * 值是否存在，未来支持列表 ANY ALL
     * @param value
     * @return
     */
    private boolean containsValue(Object value){
        return JSONPath.containsValue(this.rootObject, this.path, value);
    }
    /**
     * 查看一个对象的所有Key
     * @return
     */
    private Object keySet(){
        return JSONPath.keySet(this.rootObject, this.path);
    }

    /**
     *
     * @return
     */
    private int size(){
        return JSONPath.size(this.rootObject, this.path);
    }

    /***
     * --------------------------------------------------------
     *                      Path路径操作
     * --------------------------------------------------------
     */
    private void setPath(String newPath){
        this.path = newPath;
    }
    private void setPathToRoot(){
        this.path = "$";
    }
    private void setPathToParent(){
    	if(this.path.endsWith("]")) {
    		this.path = StringUtils.substringBeforeLast(this.path, "[");
    	}else if(this.path.contains(".")) {
    		this.path = StringUtils.substringBeforeLast(this.path, ".");
    	}
    }

}
