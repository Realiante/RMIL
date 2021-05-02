package dev.rea.rmil.server.servlets.diagnostics;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handles ping requests, blocking
 */
public class PingServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var logger = LoggerFactory.getLogger(PingServlet.class);
        long tts = 0;
        logger.debug(String.format("Ping request from: \"%s\", tts: %s", req.getRemoteUser(), tts));
        tts = req.getDateHeader("Date") - System.currentTimeMillis();

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(String.format("{ \"tts\": \"%s\"}", tts));
    }

}
