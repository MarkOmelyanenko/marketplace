export default function Table({
  columns,
  rows,
  emptyMessage = "No data.",
  numberColumns = [],
  className = "",
}) {
  const numSet = new Set(numberColumns);
  return (
    <div className={`ds-table-wrap ${className}`.trim()}>
      <table className="ds-table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} style={col.style}>
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                style={{
                  textAlign: "center",
                  padding: "var(--space-8)",
                  color: "var(--color-text-muted)",
                }}
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            rows.map((row, idx) => (
              <tr key={row.key ?? idx}>
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={numSet.has(col.key) ? "ds-table--num" : ""}
                    style={col.cellStyle?.(row)}
                  >
                    {col.render ? col.render(row[col.key], row) : row[col.key]}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
