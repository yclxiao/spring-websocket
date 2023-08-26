package top.mangod.springwebsocket.controller;

import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;


@RestController(value = "indexController")
@RequestMapping("/index")
public class IndexController {

    @GetMapping(value = "/hello")
    public String hello() {
        return "hello";
    }

}