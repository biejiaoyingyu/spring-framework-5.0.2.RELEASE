package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by cxf on 2018/9/18.
 */
@SessionAttributes("bookx")
@Controller
public class AnnotationTestController {
	private final String SUCCESS = "success";
//
//	@RequestMapping("/test")
//	public String testSessionAttributes(@ModelAttribute("u1")User user1, HttpServletRequest request, @ModelAttribute("u2")User user2, HttpSession session, Model model){
//		System.out.println(user1);
//		System.out.println(user2);
//		model.addAttribute("um",new User(6,"um","xiaofang@sina.com","123456789"));
//		request.setAttribute("u4",new User(4,"3","xiaofang@sina.com","123456789"));
//		session.setAttribute("u3",new User(3,"4","xiaofang@sina.com","123456789"));
//		return "forward:/hello";
//	}

	@RequestMapping("/test")
	public String testSessionAttributes(Model model, HttpSession session,HttpServletRequest request){
//		model.addAttribute("user",new User(1, "r", "250290", "xg@sss.com"));
		System.out.println(request.getAttribute("bookx"));
		System.out.println(session.getAttribute("bookx"));
//		System.out.println(request.getAttribute("user2"));
//		System.out.println(session.getAttribute("user2"));
//		System.out.println(request.getAttribute("user3"));
//		System.out.println(session.getAttribute("user3"));
		return SUCCESS;
	}


//	@ModelAttribute
//	public void myModelAttri(Map<String, Object> map, HttpServletRequest request){
//		System.out.println("1111111");
//		//1、提前将数据从数据库中查出来
//		Book book = new Book(1, "三国演义");
//		//2、把这个数据保存在隐含模型中
//		map.put("book", book);
//	}

	@RequestMapping("/updateBook")
	public String updateBookExt(@ModelAttribute("bookm") Book boook2,HttpServletRequest request,HttpSession session ){
		System.out.println("updateBookExt....");
		System.out.println("SpringMVC自动封装的结果："+ boook2);
		//save(book)
		//1）、本来要将页面带来的数据封装成book对象
		//2）、默认是根据反射创建一个新的book对象。将页面的值设置到这个对象中
		//3）、如果隐含模型中有这个对象，直接取出这个对象。将页面的值封装到这个对象中，不用再去创建新对象了
		//4）、根据key从隐含模型中获取到Book参数对应的值。key就是参数的类名首字母小写
		return "success";
	}

//	@ModelAttribute("user")
//	public void getUser(HttpServletRequest request){
//		System.out.println("222222222");
//		User user = new User(1, "xugang", "250290", "xg@sss.com");
//
//  		request.setAttribute("user", new User(1, "2", "250290", "xg@sss.com"));
////		model.addAttribute("user3",new User(1, "3", "250290", "xg@sss.com"));
//	}

//	@ModelAttribute
//	public void getUser(@ModelAttribute("id") Integer id,Map<String, Object> map){
//		if(id!=null){
//			User user1 = new User(1,"1","xiaofang@sina.com","123456789");
//			User user2 = new User(2,"2","xiaofang@sina.com","123456789");
//			map.put("u1", user1);
//			map.put("u2", user2);
//
//		}
//	}

	@RequestMapping("/hello")
	public String  hello(HttpServletRequest request,HttpSession session,Model model){
		System.out.println(request.getAttribute("user"));
		model.addAttribute("bookx",new Book(1, "三国演义"));
		session.setAttribute("bookm",new Book(1, "4国演义"));
//		System.out.println(request.getAttribute("user2"));
//		System.out.println(session.getAttribute("user2"));
//		System.out.println(request.getAttribute("user3"));
//		System.out.println(session.getAttribute("user3"));
		return  SUCCESS;
	}

	/**
	 * 1.放到模型中的数据才会被放到@SessionAttributes相应的key之中的session中，放在request中是不会放进去的，
	 *   而且模型中的数据应该是在目标方法调用完成后才放到request中的
	 * 2.放到@ModelAndView（和一般方法的隐含模型的区别是提前运行）中的数据会放到@SessionAttributes相应的key
	 * 	 之中的session中，可能是放到了模型中的原因,放到request中的数据不会放到@ModelAndView中，也不会放倒模型中把
	 * 3.在第二次以后的访问中session中的数据（@SessionAttributes相应的key）并不会同步到request中（和以前很有区别）
	 * 4.
	 * @RequestMapping("/updateBook")
	 * public String updateBookExt(@ModelAttribute Book boook2,HttpServletRequest request,HttpSession session ){
	 * System.out.println("updateBookExt....");
	 * System.out.println("SpringMVC自动封装的结果："+ boook2);
	 * //save(book)
	 * //1）、本来要将页面带来的数据封装成book对象
	 * //2）、默认是根据反射创建一个新的book对象。将页面的值设置到这个对象中
	 * //3）、如果隐含模型中有这个对象，直接取出这个对象。将页面的值封装到这个对象中，不用再去创建新对象了
	 * //?) 、如果隐含模型中没有而且@SessionAttributes注解有这个键（默认为@ModelAttribute 1.注解的value值，2。如果没有value值
	 * 		  就是注解相应的参数的类型小写） 会到session中取(无论通过model放到session中，还是直接放到session中都可以)，如果娶不到会报异常
	 * 		  最后才回到第一步。
	 * //return "success";
	 * //}
	 */

}
