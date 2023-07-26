package org.example.controller;

import org.example.service.UserActivationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/user")
@RestController
public class ActivationController {
    private final UserActivationService activationService;

    public ActivationController(UserActivationService activationService) {
        this.activationService = activationService;
    }
    @RequestMapping("/activation")
    public ResponseEntity<?> activation(@RequestParam("id") String id) {
        var res = activationService.activation(id);
        if (res) {
           return ResponseEntity.ok().body("Ругистрация успешно заверешена");
        }
        return ResponseEntity.internalServerError().build();
    }
}
