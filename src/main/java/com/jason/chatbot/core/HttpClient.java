package com.jason.chatbot.core;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author jasonsong
 * 2020/7/2
 */

@Slf4j
@Component
public class HttpClient {

    public static PoolingHttpClientConnectionManager poolConnManager = null;
    private static CloseableHttpClient httpClient;

    static {
        try {
            SSLContextBuilder builder = new SSLContextBuilder()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
            // 配置同时支持 HTTP 和 HTPPS
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslsf).build();
            // 初始化连接管理器
            poolConnManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            // 同时最多连接数
            poolConnManager.setMaxTotal(1000);
            // 设置最大路由
            poolConnManager.setDefaultMaxPerRoute(200);
            // 初始化httpClient
            httpClient = getConnection();
        } catch (Exception e) {
            log.error("init VoipHttpClient occur error", e);
        }
    }

    public static CloseableHttpClient getConnection() {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(30000).
                setConnectionRequestTimeout(30000).setSocketTimeout(30000).build();
        return HttpClients.custom()
                // 设置连接池管理
                .setConnectionManager(poolConnManager)
                .setDefaultRequestConfig(config)
                // 设置重试次数
                .setRetryHandler(new DefaultHttpRequestRetryHandler(2, false)).build();
    }

    public static String httpGet(String url) {
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(httpGet);
            String result = EntityUtils.toString(response.getEntity());
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpStatus.SC_OK) {
                return result;
            } else {
                log.error("请求 {} 返回错误码：{},{}", url, code, result);
                return null;
            }
        } catch (IOException e) {
            log.error("http请求异常，{}", url, e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String post(String uri, Object params, Header... heads) {
        HttpPost httpPost = new HttpPost(uri);
        CloseableHttpResponse response = null;
        try {
            StringEntity paramEntity = new StringEntity(JSON.toJSONString(params), "utf-8");
            paramEntity.setContentEncoding("UTF-8");
            paramEntity.setContentType("application/json");
            httpPost.setEntity(paramEntity);
            if (heads != null) {
                httpPost.setHeaders(heads);
            }
            response = httpClient.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            String result = EntityUtils.toString(response.getEntity());
            if (code == HttpStatus.SC_OK) {
                return result;
            } else {
                log.error("请求 {} 返回错误码:{},请求参数:{},{}", uri, code, params, result);
                return "";
            }
        } catch (Exception e) {
            log.error("http请求异常", e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                } else {
                    httpPost.abort();
                }
            } catch (Exception e) {
                log.error("response.close() error", e);
            }
        }
        return "";
    }

//    /**
//     * 每10分钟打印线程池使用情况
//     */
//    @Scheduled(cron = "0 0/10 * * * ? ")
//    private void scheduledTask() {
//        log.info("HTTP 线程池使用情况，totalStats:{}", poolConnManager.getTotalStats());
//    }
}
