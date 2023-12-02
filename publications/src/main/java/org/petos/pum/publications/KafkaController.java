package org.petos.pum.publications;

import lombok.RequiredArgsConstructor;
import org.petos.pum.networks.dto.credentials.ValidationRequest;
import org.petos.pum.networks.dto.packages.PublishingRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paval Shlyk
 * @since 01/12/2023
 */
@Controller
@RequiredArgsConstructor
public class KafkaController {
private final KafkaTemplate<String, ValidationRequest> publisherTemplate;
private final KafkaTemplate<String, PublishingRequest> repositoryTemplate;
private final Map<String, Object> requestMemory = new HashMap<>();
}
