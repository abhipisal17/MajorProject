import React, { useEffect, useState } from 'react';
import { api, token } from './api';
import AdminKycReview from './AdminKycReview';

function Auth({ onAuthenticated }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [mode, setMode] = useState('login');
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    setError('');
    try {
      const result = await api(`/auth/${mode}`, {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      localStorage.setItem('token', result.token);
      onAuthenticated();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return <main className="mx-auto mt-24 max-w-md card">
    <p className="font-bold text-cyan-400">VAULT / P2P</p>
    <h1 className="mt-2 text-3xl font-bold">Trade with protected settlement.</h1>
    <p className="mt-2 text-sm text-slate-400">Identity verification is required before trading.</p>
    {error && <p className="mt-5 rounded-lg bg-red-500/10 p-3 text-red-200">{error}</p>}
    <form className="mt-6 space-y-3" onSubmit={submit}>
      <input className="w-full rounded-lg bg-slate-800 p-3" type="email" placeholder="Email" value={email} onChange={event => setEmail(event.target.value)} required />
      <input className="w-full rounded-lg bg-slate-800 p-3" type="password" placeholder="Password (12+ characters)" value={password} onChange={event => setPassword(event.target.value)} minLength="12" required />
      <button className="button w-full">{mode === 'login' ? 'Sign in' : 'Create account'}</button>
    </form>
    <button className="mt-4 text-sm text-cyan-300" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
      {mode === 'login' ? 'Need an account? Register' : 'Already registered? Sign in'}
    </button>
  </main>;
}

function KycForm({ status, onStatusChange, onSignOut }) {
  const [legalName, setLegalName] = useState('');
  const [country, setCountry] = useState('');
  const [documentType, setDocumentType] = useState('');
  const [document, setDocument] = useState(null);
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    setError('');
    try {
      await api('/kyc/submit', {
        method: 'POST',
        body: JSON.stringify({ legalName, country, documentType }),
      });
      const body = new FormData();
      body.append('document', document);
      const response = await fetch('http://localhost:8080/api/kyc/documents', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token()}` },
        body,
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.message || 'Document upload failed');
      onStatusChange(result);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return <main className="mx-auto mt-20 max-w-xl card">
    <div className="flex items-start justify-between gap-4"><div><p className="font-bold text-cyan-400">ACCOUNT VERIFICATION</p><h1 className="mt-2 text-3xl font-bold">Verify your identity to trade.</h1></div><button className="text-sm text-slate-400" onClick={onSignOut}>Sign out</button></div>
    <p className="mt-4 text-slate-400">Current status: <b className="text-slate-100">{status.status}</b>. Trading remains locked until an administrator verifies your account.</p>
    {status.status === 'PENDING' && <p className="mt-5 rounded-lg border border-amber-500/30 bg-amber-500/10 p-3 text-amber-200">Your verification request is under review.</p>}
    {status.status === 'REJECTED' && <p className="mt-5 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-red-200">Your last request was rejected. Submit updated information.</p>}
    {error && <p className="mt-5 rounded-lg bg-red-500/10 p-3 text-red-200">{error}</p>}
    <form className="mt-6 space-y-3" onSubmit={submit}>
      <input className="w-full rounded-lg bg-slate-800 p-3" placeholder="Legal name" value={legalName} onChange={event => setLegalName(event.target.value)} required />
      <input className="w-full rounded-lg bg-slate-800 p-3" placeholder="Country" value={country} onChange={event => setCountry(event.target.value)} required />
      <select className="w-full rounded-lg bg-slate-800 p-3" value={documentType} onChange={event => setDocumentType(event.target.value)} required><option value="">Select document type</option><option>Passport</option><option>National ID</option><option>Driver's licence</option></select>
      <input className="w-full rounded-lg bg-slate-800 p-3 text-sm" type="file" accept="application/pdf,image/jpeg,image/png" onChange={event => setDocument(event.target.files?.[0] ?? null)} required />
      <button className="button w-full" >Submit for verification</button>
    </form>
  </main>;
}

function Marketplace({ onSignOut, isAdmin, onReview }) {
  const [orders, setOrders] = useState([]);
  const [wallets, setWallets] = useState([]);
  const [error, setError] = useState('');
  const load = () => Promise.all([api('/orders'), api('/wallets')]).then(([nextOrders, nextWallets]) => { setOrders(nextOrders); setWallets(nextWallets); }).catch(requestError => setError(requestError.message));
  useEffect(() => { load(); }, []);
  const accept = async id => { try { await api(`/orders/${id}/accept`, { method: 'POST' }); load(); } catch (requestError) { setError(requestError.message); } };
  return <main className="mx-auto max-w-6xl p-6"><header className="mb-8 flex items-center justify-between"><div><p className="font-bold text-cyan-400">VAULT / P2P</p><h1 className="text-3xl font-bold">Escrow marketplace</h1></div><div className="flex items-center gap-4">{isAdmin && <button className="text-sm text-cyan-300" onClick={onReview}>Review KYC</button>}<button className="text-sm text-slate-400" onClick={onSignOut}>Sign out</button></div></header>{error && <p className="mb-5 rounded-lg bg-red-500/10 p-3 text-red-200">{error}</p>}<section className="grid gap-4 md:grid-cols-3">{wallets.map(wallet => <div className="card" key={wallet.id}><p className="text-sm text-slate-400">{wallet.asset} wallet</p><b className="text-2xl">{wallet.available}</b><p className="mt-2 text-sm text-amber-300">{wallet.locked} in escrow</p></div>)}</section><section className="mt-8 card"><div className="mb-5 flex justify-between"><div><h2 className="text-xl font-bold">Active offers</h2><p className="text-sm text-slate-400">Accept a matching offer to begin protected settlement.</p></div><button className="button" onClick={load}>Refresh</button></div><div className="space-y-3">{orders.length ? orders.map(order => <article className="flex flex-wrap items-center justify-between gap-4 rounded-xl bg-slate-800/70 p-4" key={order.id}><div><p className="font-bold">{order.cryptoAmount} {order.asset} for {order.fiatAmount} {order.fiatCurrency}</p><p className="text-sm text-slate-400">{order.paymentMethods}</p></div><button className="button" disabled={order.status !== 'OPEN'} onClick={() => accept(order.id)}>Accept offer</button></article>) : <p className="py-8 text-center text-slate-400">No active offers yet.</p>}</div></section></main>;
}

export default function KycApp() {
  const [authenticated, setAuthenticated] = useState(Boolean(token()));
  const [status, setStatus] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [reviewing, setReviewing] = useState(false);

  useEffect(() => {
    if (!authenticated) return;
    api('/kyc/status').then(setStatus).catch(() => {
      localStorage.removeItem('token');
      setAuthenticated(false);
    });
    api('/admin/kyc/pending').then(() => setIsAdmin(true)).catch(() => setIsAdmin(false));
  }, [authenticated]);

  const signOut = () => {
    localStorage.removeItem('token');
    setStatus(null);
    setIsAdmin(false);
    setReviewing(false);
    setAuthenticated(false);
  };

  if (!authenticated) return <Auth onAuthenticated={() => setAuthenticated(true)} />;
  if (!status) return <main className="mx-auto mt-24 max-w-md text-center text-slate-400">Loading account status…</main>;
  if (reviewing) return <AdminKycReview onClose={() => setReviewing(false)} />;
  return status.status === 'VERIFIED'
    ? <Marketplace onSignOut={signOut} isAdmin={isAdmin} onReview={() => setReviewing(true)} />
    : <KycForm status={status} onStatusChange={setStatus} onSignOut={signOut} />;
}