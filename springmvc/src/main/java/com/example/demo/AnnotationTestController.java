package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by cxf on 2018/9/18.
 */
@SessionAttributes("user")
@Controller
public class AnnotationTestController {
	private final String SUCCESS = "success";

	@RequestMapping("/test")
	public String testSessionAttributes(Map<String, Object> map){
		User user= new User(1,"xiaofang","12345","1234567");
		map.put("user",user);
		return "redirect:/hello";
	}

	@ModelAttribute
	public void myModelAttri(Map<String, Object> map){
		System.out.println("1111111");
		//1、提前将数据从数据库中查出来
		Book book = new Book(1, "三国演义");
		//2、把这个数据保存在隐含模型中
		map.put("MyBook", book);
	}

	@ModelAttribute
	public User getUser(@RequestParam(value="id",required=false) Integer id ){
		System.out.println("222222222");
		User user = new User();
		if(id != null){
			//从数据库中查询出要修改的User对象，现在假设new的User对象就是从数据库中查询出来的User对象
			user = new User(1, "xugang", "250290", "xg@sss.com");
		}
		return user;
	}

	@ModelAttribute
	public void getUser(@RequestParam(value="id",required=false) Integer id,Map<String, Object> map){
		System.out.println("3333333333");
		if(id!=null){
			User user = new User(1,"xiaofang","xiaofang@sina.com","123456789");
			map.put("u", user);
		}
	}

	@RequestMapping("/hello")
	public String hello(HttpServletRequest request){
		Object user = request.getAttribute("user");
		System.out.println(user);
		System.out.println(request);
		return SUCCESS;
	}

}
