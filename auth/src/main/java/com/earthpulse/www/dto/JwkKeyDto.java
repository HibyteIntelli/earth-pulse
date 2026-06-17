package com.earthpulse.www.dto;

public record JwkKeyDto(String kty, String use, String kid, String alg, String n, String e) {}
