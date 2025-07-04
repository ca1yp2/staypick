package com.staypick.staypick_back.security;

import com.staypick.staypick_back.entity.User;
import com.staypick.staypick_back.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("\n✅ [JWT Filter] 작동 시작");

        String token = jwtUtil.extractTokenFromRequest(request);
        System.out.println("🧾 Authorization Header에서 추출된 토큰: " + token);

        if (token != null && jwtUtil.validateToken(token)) {
            String userid = jwtUtil.extractUsername(token); // ✅ 여기서 userid 추출
            String role = jwtUtil.extractRole(token);

            System.out.println("✅ 토큰 유효함");
            System.out.println("🔐 userid: " + userid);
            System.out.println("🔐 role: " + role);

            try {
                User userEntity = userRepository.findByUserid(userid)
                        .orElseThrow(() -> new RuntimeException(" 사용자 정보 없음: " + userid));

                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // 핵심: userid를 인증 토큰의 principal로 사용
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userid, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println(" SecurityContext에 사용자 인증 정보 저장 완료 ");
            } catch (Exception e) {
                System.out.println(" 사용자 인증 처리 실패: " + e.getMessage());
            }
        } else {
            System.out.println("토큰이 없거나 유효하지 않음");
        }

        filterChain.doFilter(request, response);
    }
}
