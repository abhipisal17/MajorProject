import React, { useEffect, useState } from 'react';
import { api, token } from './api';

export default function AdminKycReview({ onClose }) {
  const [requests, setRequests] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      setRequests(await api('/admin/kyc/pending'));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const review = async (userId, decision) => {
    try {
      await api(`/admin/kyc/${userId}/${decision}`, { method: 'POST' });
      load();
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  const openDocument = async userId => {
    try {
      const response = await fetch(`http://localhost:8080/api/admin/kyc/${userId}/document`, {
        headers: { Authorization: `Bearer ${token()}` },
      });
      if (!response.ok) {
        const result = await response.json().catch(() => ({}));
        throw new Error(result.message || 'Unable to open document');
      }
      window.open(URL.createObjectURL(await response.blob()), '_blank', 'noopener,noreferrer');
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  return <main className="mx-auto max-w-4xl p-6">
    <header className="mb-8 flex items-center justify-between">
      <div><p className="font-bold text-cyan-400">ADMIN / KYC</p><h1 className="text-3xl font-bold">Verification review</h1></div>
      <button className="text-sm text-slate-400" onClick={onClose}>Back to marketplace</button>
    </header>
    {error && <p className="mb-5 rounded-lg bg-red-500/10 p-3 text-red-200">{error}</p>}
    {loading ? <p className="text-slate-400">Loading requests…</p> : requests.length === 0 ? <div className="card text-slate-400">No KYC requests are waiting for review.</div> : <section className="space-y-4">{requests.map(request => <article className="card flex flex-wrap items-center justify-between gap-4" key={request.userId}><div><p className="font-bold">{request.email}</p><p className="text-sm text-slate-400">{request.userId}</p><p className={request.documentAttached ? 'mt-2 text-sm text-emerald-300' : 'mt-2 text-sm text-red-300'}>{request.documentAttached ? 'Document attached' : 'No document attached'}</p></div><div className="flex flex-wrap gap-2"><button className="rounded-lg border border-slate-700 px-4 py-2 text-sm" disabled={!request.documentAttached} onClick={() => openDocument(request.userId)}>Open document</button><button className="button" disabled={!request.documentAttached} onClick={() => review(request.userId, 'verify')}>Verify</button><button className="rounded-lg bg-red-500/20 px-4 py-2 text-sm font-semibold text-red-200" onClick={() => review(request.userId, 'reject')}>Reject</button></div></article>)}</section>}
  </main>;
}
