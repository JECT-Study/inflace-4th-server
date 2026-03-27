package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
}
