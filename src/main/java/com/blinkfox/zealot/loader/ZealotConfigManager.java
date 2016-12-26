package com.blinkfox.zealot.loader;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.Node;
import com.blinkfox.zealot.bean.XmlContext;
import com.blinkfox.zealot.config.AbstractZealotConfig;
import com.blinkfox.zealot.consts.ZealotConst;
import com.blinkfox.zealot.exception.ConfigNotFoundException;
import com.blinkfox.zealot.exception.NodeNotFoundException;
import com.blinkfox.zealot.helpers.StringHelper;
import com.blinkfox.zealot.helpers.XmlNodeHelper;
import com.blinkfox.zealot.log.Log;

/**
 * Zealot配置缓存管理器，用于加载Zealot Config配置信息到缓存中
 * @author blinkfox
 * @date 2016-12-26
 */
public class ZealotConfigManager {
    
    // 得到 log 实例
    private static final Log log = Log.get(ZealotConfigManager.class);
	
	// 初始化的单例
	private static final ZealotConfigManager confManager = new ZealotConfigManager();
	
	/**
	 * 私有化构造方法
	 */
	private ZealotConfigManager() {
		super();
	}
	
	/**
	 * 获取LoadZealotConfInfo的唯一实例
	 * @return
	 */
	public static ZealotConfigManager getInstance() {
		return confManager;
	}
	
	/**
	 * 初始化加载Zealot的配置信息到缓存中
	 * @param configClass 系统中Zealot的class路径
	 */
	public void initLoad(String configClass) {
		// 加载ZealotConfig配置信息
		loadZealotConfig(configClass);

        // 获取遍历每个zealotxml配置文件，将其key和文档缓存到ConcurrentHashMap内存缓存中
        cachingXmlZealots();
	}
	
	/**
     * 初始化zealotConfig的之类，并执行初始化mapper到缓存中
     * @param event 上下文事件对象
     * @param xmlContext xmlContext实例
     */
    private void loadZealotConfig(String configClass) {
        log.info("----zealot加载器开始加载，Zealot配置类为:" + configClass);
        if (configClass == null) {
            throw new ConfigNotFoundException("未获取到ZealotConfig配置信息");
        }

        Object temp = null;
        try {
            temp = Class.forName(configClass).newInstance();
        } catch (Exception e) {
            throw new ConfigNotFoundException("初始化zealotConfig实例失败,配置名称为:" + configClass, e);
        }

        // 判断获取到的类是否是AbstractZealotConfig的子类
        if ((temp != null) && (temp instanceof AbstractZealotConfig)) {
        	AbstractZealotConfig zealotConfig = (AbstractZealotConfig) temp;
            zealotConfig.configXml(XmlContext.getInstance());
            zealotConfig.configTagHandler();
            log.info("------zealot的xml文件和tagHandler加载完成");
        }
    }

    /**
     * 将每个zealotxml配置文件的key和文档缓存到ConcurrentHashMap内存缓存中
     */
    private void cachingXmlZealots() {
        Map<String, String> xmlMaps = XmlContext.getInstance().getXmlMap();

        // 遍历所有的xml文档，将每个zealot节点缓存到ConcurrentHashMap内存缓存中
        for (Iterator<Map.Entry<String, String>> it = xmlMaps.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            String nameSpace = entry.getKey();
            String value = entry.getValue();
            Document document = XmlNodeHelper.getDocument(value);
            if (document == null) {
                throw new ConfigNotFoundException("未找到zealot xml的配置文件，nameSpace为:" + nameSpace);
            }

            // 获取该文档下所有的zealot元素,
            @SuppressWarnings("unchecked")
			List<Node> zealotNodes = document.selectNodes(ZealotConst.ZEALOT_TAG);
            for (Node zealotNode: zealotNodes) {
                Node idNode = zealotNode.selectSingleNode(ZealotConst.ATTR_ID);
                String zealotId = XmlNodeHelper.getNodeText(idNode);
                if (StringHelper.isBlank(zealotId)) {
                    throw new NodeNotFoundException("未获取到zealot节点,zealotId为:" + zealotId);
                }

                // zealot节点缓存到Map中，key是由nameSpace和zealot id组成,用"@"符号分隔,value是zealotNode
                String zealotKey = StringHelper.concat(nameSpace, ZealotConst.SP_AT, zealotId);
                AbstractZealotConfig.getZealots().put(zealotKey, zealotNode);
            }
        }
    }

}