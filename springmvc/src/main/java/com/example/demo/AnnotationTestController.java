package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Created by cxf on 2018/9/18.
 */
@SessionAttributes("user")
@Controller
public class AnnotationTestController {
	private final String SUCCESS = "success";

	@RequestMapping("/test")
	public String testSessionAttributes(@ModelAttribute("u1")User user1, HttpServletRequest request, @ModelAttribute("u2")User user2, HttpSession session, Model model){
		System.out.println(user1);
		System.out.println(user2);
		model.addAttribute("um",new User(6,"um","xiaofang@sina.com","123456789"));
		request.setAttribute("u4",new User(4,"3","xiaofang@sina.com","123456789"));
		session.setAttribute("u3",new User(3,"4","xiaofang@sina.com","123456789"));
		return "forward:/hello";
	}

	@ModelAttribute
	public void myModelAttri(Map<String, Object> map,HttpServletRequest request){
		System.out.println("1111111");
		//1、提前将数据从数据库中查出来
		Book book = new Book(1, "三国演义");
		//2、把这个数据保存在隐含模型中
		map.put("MyBook", book);
	}
	@RequestMapping("/updateBook")
	public String updateBookExt(@ModelAttribute("MyBook1")Book book2,HttpServletRequest request,HttpSession session ){
		System.out.println("updateBookExt....");
		System.out.println("SpringMVC自动封装的结果："+book2);
		System.out.println((User)(request.getAttribute("user")));
		System.out.println((User)session.getAttribute("user"));

		//save(book)
		//1）、本来要将页面带来的数据封装成book对象
		//2）、默认是根据反射创建一个新的book对象。将页面的值设置到这个对象中
		//3）、如果隐含模型中有这个对象，直接取出这个对象。将页面的值封装到这个对象中，不用再去创建新对象了
		//4）、根据key从隐含模型中获取到Book参数对应的值。key就是参数的类名首字母小写
		return "success";
	}

	@ModelAttribute("user")
	public User getUser(@ModelAttribute("id") Integer id,HttpServletRequest request ){
		System.out.println("222222222");
		User user = new User();

		if(id != null){
			//从数据库中查询出要修改的User对象，现在假设new的User对象就是从数据库中查询出来的User对象
			user = new User(1, "xugang", "250290", "xg@sss.com");
		}
		request.setAttribute("user",user);
		return user;
	}

	@ModelAttribute
	public void getUser(@ModelAttribute("id") Integer id,Map<String, Object> map){
		if(id!=null){
			User user1 = new User(1,"1","xiaofang@sina.com","123456789");
			User user2 = new User(2,"2","xiaofang@sina.com","123456789");
			map.put("u1", user1);
			map.put("u2", user2);

		}
	}

	@RequestMapping("/hello")
	public String hello( @ModelAttribute("u1")User user3,@ModelAttribute("u4")User user4,@ModelAttribute("um")User userm){
		System.out.println(user3);
		System.out.println(user4);
		System.out.println(userm);
		return SUCCESS;
	}


	@ExceptionHandler()
	public ModelAndView  handException(Exception exception){
		ModelAndView modelAndView = new ModelAndView("error");
		modelAndView.addObject("exception",exception);
		return modelAndView;

	}

}
