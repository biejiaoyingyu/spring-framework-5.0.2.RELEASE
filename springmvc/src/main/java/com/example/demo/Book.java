package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by cxf on 2018/9/18.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Book {
	private int id;
	private String name;
}
