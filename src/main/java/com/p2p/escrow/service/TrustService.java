package com.p2p.escrow.service;
import com.p2p.escrow.domain.AppUser; import org.springframework.stereotype.Service;
@Service public class TrustService { public void apply(AppUser u, int delta){u.trustScore=Math.max(-100,Math.min(500,u.trustScore+delta));u.suspended=u.trustScore<=-40;} public boolean canTrade(AppUser u){return !u.suspended&&u.kycStatus==com.p2p.escrow.domain.Enums.KycStatus.VERIFIED;} public int dailyLimit(AppUser u){return u.trustScore<10?500:u.trustScore<100?5000:50000;} }
