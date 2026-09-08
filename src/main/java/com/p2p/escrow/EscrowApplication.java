package com.p2p.escrow;
import org.springframework.boot.SpringApplication; import org.springframework.boot.autoconfigure.SpringBootApplication; import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
@SpringBootApplication @EnableJpaRepositories(considerNestedRepositories=true) public class EscrowApplication { public static void main(String[] args){ SpringApplication.run(EscrowApplication.class,args); } }
