package cn.dayangshuo.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 这是一个服务消费者，EnableDiscoveryClient(autoRegister = false)表示不自动注册到注册中心
 * @author DAYANG
 */
@SpringBootApplication
@EnableDiscoveryClient(autoRegister = false)
public class NacosConsumerApplication {

    /**
     * 构造一个RestTemplate，用于访问http服务
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(NacosConsumerApplication.class, args);
    }

    @RestController
    class HelloController {

        /**
         * 自动注入DiscoveryClient，这是Spring Cloud Commons模块提供的一个服务发现接口
         * Spring Cloud Alibaba Nacos Discory模块内部会初始化这个接口的
         * 实现类NacosDiscoreryClient，用于后续服务发现操作
         */
        @Autowired
        private DiscoveryClient discoveryClient;
        /**
         * 注入前面构造的RestTemplate
         */
        @Autowired
        private RestTemplate restTemplate;

        private final String serviceName = "nacos-service-provider";

        @GetMapping("/info")
        public String info() {
            //使用DiscoveryClient获取nacos-service-provider服务对用的所有实例
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Allin Services: ").append(discoveryClient.getServices()).append("<br/>");
            stringBuilder.append("nacos-service-provider instance list : <br/>");
            //使用lambda表达式获取所有实例，获取各个实例的host和port信息
            serviceInstances.forEach(instance -> {
                stringBuilder.append("serviceId: ").append(instance.getServiceId())
                        .append(", host: ").append(instance.getHost())
                        .append(", port: ").append(instance.getPort());
                stringBuilder.append("<br/>");
            });
            return stringBuilder.toString();
        }

        @GetMapping("/hello")
        public String hello() {
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
            //从所有nacos-service-provider实例中任意获取一个
            //如果没有服务实例，将会抛出IllegalArgumentException异常
            ServiceInstance instance = serviceInstances.stream().findAny()
                    .orElseThrow(() -> new IllegalArgumentException("no " + serviceName + " instance available"));
            String url = String.format("http://%s:%s/echo?name=nacos", instance.getHost(), instance.getPort());
            //使用RestTemplate调用服务实例中的/echo方法
            return restTemplate.getForObject(url, String.class);
        }

    }
}
