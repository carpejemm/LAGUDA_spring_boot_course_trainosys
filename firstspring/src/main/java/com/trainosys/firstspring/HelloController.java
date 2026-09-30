package com.trainosys.firstspring;

import org.springframework.web.bind.annotation.*;

// Annotation that indicates this class is a REST controller, which handles HTTP requests and responses
@RestController
public class HelloController {
    // GET request to /hello endpoint
    // POST request to /hello endpoint
    // PUT/PATCH request to /hello endpoint ==> same usage - updating existing resource
    // DELETE request to /hello endpoint

    // Annotation that maps HTTP GET requests to the /hello endpoint
    // /hello will return a simple "Hello, World!" message when accessed
    @GetMapping("/hello/{name}")
    public HelloResponse hello(@PathVariable String name) {
        return new HelloResponse("Hello, " + name + "!");
    }
    // Annotation that maps HTTP POST requests to the /hello endpoint
    // /hello will return a simple "Hello, World! (POST)" message when accessed
    @PostMapping("/hello")
    public HelloResponse helloPost(@RequestBody String name) {
        return new HelloResponse("Hello, " + name + "!");
    }
}
