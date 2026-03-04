/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.upb.tickmaster.httpserver;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
/**
 *
 * @author rlaredo
 */
public class ApacheServer {
    private HttpServer server = null;
    private boolean isServerDone = false;

    public ApacheServer(){
    }
    
    public boolean start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(1914), 0);
            this.server.createContext("/", exchange -> {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
                new RootHandler().handle(exchange);
            });

//            this.server.createContext("/hola", new UsuariosHandler());
            this.server.createContext("/usuarios", new UsuariosProxyHandler());
            this.server.createContext("/eventos", new EventosProxyHandler());
            this.server.createContext("/estado", new EstadoHandler());
            this.server.setExecutor(Executors.newFixedThreadPool(2));
            this.server.start();
        
        return true;
        } catch (IOException e) {
            this.server = null;
            //System.exit(-1);
        }
        return false;
    }
    
    public void stop(){
        this.server.stop(0);
        this.server = null;
    }

  
    
}
