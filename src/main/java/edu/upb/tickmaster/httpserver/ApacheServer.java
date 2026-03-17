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
 * Legacy HTTP compatibility gateway. The active distributed stack lives under multi/.
 *
 * @author rlaredo
 */
@Deprecated
public class ApacheServer {
    private HttpServer server = null;
    private boolean isServerDone = false;
    private final int port;

    public ApacheServer(){
        this.port = Integer.parseInt(System.getenv().getOrDefault("LEGACY_HTTP_PORT", "1914"));
    }
    
    public boolean start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
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
            this.server.createContext("/tickets", new TicketsProxyHandler());
            this.server.createContext("/estado", new EstadoHandler());
            this.server.setExecutor(Executors.newFixedThreadPool(2));
            this.server.start();
            System.out.println("[legacy] HTTP compatibility gateway listening on port " + port);
        
        return true;
        } catch (IOException e) {
            this.server = null;
            System.out.println("[legacy] Could not start HTTP compatibility gateway: " + e.getMessage());
            //System.exit(-1);
        }
        return false;
    }
    
    public void stop(){
        this.server.stop(0);
        this.server = null;
    }

  
    
}
