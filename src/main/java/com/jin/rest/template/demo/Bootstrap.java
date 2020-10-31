package com.jin.rest.template.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author wu.jinqing
 * @date 2020年10月12日
 */
@SpringBootApplication
@RestController
public class Bootstrap {
    @Autowired
    private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }

    @RequestMapping("/getUser")
    public User getUser(Long id)
    {
        User user = restTemplate.getForEntity("http://localhost:8080/getUser2?id={id}", User.class, id).getBody();

        return user;
    }

//    @RequestMapping("/getUser2")
//    public User getUser2(Long id)
//    {
//        User user = restTemplate.postForEntity("http://localhost:8080/getUser2?id={id}", User.class, id).getBody();
//
//        return user;
//    }
}
