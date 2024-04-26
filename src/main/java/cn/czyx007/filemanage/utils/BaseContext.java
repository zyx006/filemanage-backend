package cn.czyx007.filemanage.utils;

/**
 * 基于ThreadLocal封装工具类，用户保存和获取当前登录用户id
 */
public class BaseContext {
    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();
    /**
     * 设置值
     * @param id
     */
    public static void setCurrentId(String id){
        threadLocal.set(id);
    }
    /**
     * 获取值
     * @return
     */
    public static String getCurrentId(){
        return threadLocal.get();
    }
}
