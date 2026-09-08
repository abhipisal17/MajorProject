package com.p2p.escrow.domain;
public final class Enums { private Enums(){} public enum Role {USER,ADMIN} public enum KycStatus {PENDING,VERIFIED,REJECTED} public enum Side {BUY,SELL} public enum OrderStatus {OPEN,ACCEPTED,COMPLETED,CANCELLED,DISPUTED} public enum EscrowStatus {LOCKED,BUYER_PAID,RELEASED,DISPUTED,REFUNDED} }
