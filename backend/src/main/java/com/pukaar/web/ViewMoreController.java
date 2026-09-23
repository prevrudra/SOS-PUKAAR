package com.pukaar.web;

import com.pukaar.domain.elderly.InactivityViewMoreService;
import com.pukaar.domain.emergency.SosViewMoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ViewMoreController {
    private final InactivityViewMoreService inactivityViewMoreService;
    private final SosViewMoreService sosViewMoreService;

    @GetMapping(value = {"/view/{token}", "/api/v1/view/{token}"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> view(@PathVariable String token) {
        return inactivityViewMoreService.renderHtml(token)
                .or(() -> sosViewMoreService.renderHtml(token))
                .map(html -> ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html))
                .orElseGet(() -> ResponseEntity.status(404)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html><body><h1>Link expired or invalid</h1></body></html>"));
    }
}
