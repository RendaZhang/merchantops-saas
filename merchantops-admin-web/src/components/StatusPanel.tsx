type StatusPanelProps = {
  title: string
  message: string
}

export function StatusPanel({ title, message }: StatusPanelProps) {
  return (
    <section className="rounded-lg border border-neutral-200 bg-white p-5">
      <h2 className="text-lg font-semibold text-neutral-950">{title}</h2>
      <p className="mt-2 text-sm text-neutral-600">{message}</p>
    </section>
  )
}
