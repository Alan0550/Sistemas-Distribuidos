/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.upb.tickmaster.httpserver;

/**
 *
 * @author rlaredo
 */
public enum Method {
	GET("GET"), //
	HEAD("HEAD"), //
	POST("POST"), //
	PUT("PUT"), //
	DELETE("DELETE"), //
	TRACE("TRACE"), //
	CONNECT("CONNECT"), //
	UNRECOGNIZED(null); //

	private final String method;

	Method(String method) {
		this.method = method;
	}
}
