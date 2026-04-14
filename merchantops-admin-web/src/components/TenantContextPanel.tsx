import type { ContextResponse, JwtDisplayClaims } from '../lib/schemas'

type TenantContextPanelProps = {
  context: ContextResponse
  claims: JwtDisplayClaims | null
}

export function TenantContextPanel({ context, claims }: TenantContextPanelProps) {
  return (
    <section className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(280px,380px)]">
      <div className="rounded-lg border border-neutral-200 bg-white p-5">
        <p className="text-sm font-medium text-emerald-700">Current context</p>
        <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
          {context.tenantCode}
        </h2>
        <dl className="mt-5 grid gap-4 sm:grid-cols-2">
          <ContextValue label="Tenant ID" value={context.tenantId} />
          <ContextValue label="Operator" value={context.username} />
          <ContextValue label="Operator ID" value={context.userId} />
          <ContextValue label="Token tenant" value={claims?.tenantCode ?? 'Unavailable'} />
        </dl>
      </div>

      <div className="rounded-lg border border-neutral-200 bg-white p-5">
        <p className="text-sm font-medium text-neutral-600">Token roles</p>
        <TokenList values={claims?.roles ?? []} emptyLabel="No roles in token" />

        <p className="mt-6 text-sm font-medium text-neutral-600">Token permissions</p>
        <TokenList
          values={claims?.permissions ?? []}
          emptyLabel="No permissions in token"
        />
      </div>
    </section>
  )
}

function ContextValue({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs font-medium uppercase text-neutral-500">{label}</dt>
      <dd className="mt-1 break-words text-base font-semibold text-neutral-950">
        {value}
      </dd>
    </div>
  )
}

function TokenList({ values, emptyLabel }: { values: string[]; emptyLabel: string }) {
  if (values.length === 0) {
    return <p className="mt-3 text-sm text-neutral-500">{emptyLabel}</p>
  }

  return (
    <div className="mt-3 flex max-h-44 flex-wrap gap-2 overflow-y-auto pr-1">
      {values.map((value) => (
        <span
          key={value}
          className="rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-800"
        >
          {value}
        </span>
      ))}
    </div>
  )
}
