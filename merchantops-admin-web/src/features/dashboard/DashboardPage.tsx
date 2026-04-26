const workflowPlaceholders = [
  {
    title: 'Approvals',
    description: 'Human-reviewed user, import, and ticket proposal decisions.',
  },
  {
    title: 'Imports',
    description: 'USER_CSV imports, error pages, and replay paths.',
  },
  {
    title: 'AI Interactions',
    description: 'Suggestion history and tenant usage summary entry points.',
  },
] as const

const liveWorkflows = [
  {
    title: 'Tickets',
    description:
      'Read-only queue for current-tenant tickets through the existing public ticket list API.',
  },
  {
    title: 'Feature Flags',
    description:
      'Tenant-scoped rollout controls for AI and workflow bridges through the existing feature-flag API.',
  },
] as const

export function DashboardPage() {
  return (
    <section>
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-700">Navigation targets</p>
          <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
            Workflow entry points
          </h2>
        </div>
        <p className="max-w-xl text-sm text-neutral-600">
          Tickets and Feature Flags now open live workflow screens. Remaining
          pages stay behind later slices.
        </p>
      </div>

      <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {liveWorkflows.map((item) => (
          <article
            key={item.title}
            className="min-h-36 rounded-lg border border-emerald-200 bg-white p-5"
          >
            <div className="flex items-start justify-between gap-3">
              <h3 className="text-lg font-semibold text-neutral-950">{item.title}</h3>
              <span className="rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700">
                Live
              </span>
            </div>
            <p className="mt-4 text-sm leading-6 text-neutral-600">
              {item.description}
            </p>
          </article>
        ))}

        {workflowPlaceholders.map((item) => (
          <article
            key={item.title}
            className="min-h-36 rounded-lg border border-neutral-200 bg-white p-5"
          >
            <div className="flex items-start justify-between gap-3">
              <h3 className="text-lg font-semibold text-neutral-950">{item.title}</h3>
              <span className="rounded-full border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-xs font-medium text-neutral-600">
                Placeholder
              </span>
            </div>
            <p className="mt-4 text-sm leading-6 text-neutral-600">
              {item.description}
            </p>
          </article>
        ))}
      </div>
    </section>
  )
}
