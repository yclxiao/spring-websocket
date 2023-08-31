package top.mangod.springwebsocket.conf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class CustomFilter extends OncePerRequestFilter {

    public CustomFilter() {
        log.warn("=================init CustomFilter====================");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        response.setStatus(200);
        response.setHeader("Content-Type", "application/json");
        response.getWriter().write("{\"age\":12}");
        response.getWriter().flush();
        // 断点为止
        response.flushBuffer();
    }
}
