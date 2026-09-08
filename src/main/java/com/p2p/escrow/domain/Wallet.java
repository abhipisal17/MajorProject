package com.p2p.escrow.domain;
import jakarta.persistence.*; import java.math.BigDecimal; import java.util.UUID;
@Entity @Table(name="wallets") public class Wallet { @Id public UUID id=UUID.randomUUID(); @Column(name="user_id") public UUID userId; public String asset; public BigDecimal available=BigDecimal.ZERO; public BigDecimal locked=BigDecimal.ZERO; @Version public long version; }
