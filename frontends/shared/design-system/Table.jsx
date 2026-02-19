import { useState, useMemo } from 'react';
import Pagination from './Pagination';

export default function Table({
  columns,
  rows,
  emptyMessage = 'No data.',
  numberColumns = [],
  pageSize = 0,
  className = '',
}) {
  const numSet = new Set(numberColumns);
  const [sortKey, setSortKey] = useState(null);
  const [sortDir, setSortDir] = useState('asc');
  const [page, setPage] = useState(1);

  const handleSort = (key) => {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
    setPage(1);
  };

  const sortedRows = useMemo(() => {
    if (!sortKey) return rows;
    const col = columns.find((c) => c.key === sortKey);
    if (!col) return rows;
    return [...rows].sort((a, b) => {
      let aVal = a[sortKey];
      let bVal = b[sortKey];
      if (aVal == null) return 1;
      if (bVal == null) return -1;
      if (typeof aVal === 'string') aVal = aVal.toLowerCase();
      if (typeof bVal === 'string') bVal = bVal.toLowerCase();
      if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });
  }, [rows, sortKey, sortDir, columns]);

  const paginatedRows = useMemo(() => {
    if (!pageSize || pageSize <= 0) return sortedRows;
    const start = (page - 1) * pageSize;
    return sortedRows.slice(start, start + pageSize);
  }, [sortedRows, page, pageSize]);

  const totalPages = pageSize > 0 ? Math.ceil(rows.length / pageSize) : 1;

  return (
    <div>
      <div className={`ds-table-wrap ${className}`.trim()}>
        <table className="ds-table">
          <thead>
            <tr>
              {columns.map((col) => {
                const isSortable = col.sortable !== false && col.key !== 'actions';
                const isActive = sortKey === col.key;
                return (
                  <th
                    key={col.key}
                    style={col.style}
                    className={isSortable ? 'ds-table--sortable' : ''}
                    onClick={isSortable ? () => handleSort(col.key) : undefined}
                  >
                    {col.label}
                    {isSortable && (
                      <span className={`ds-sort-indicator ${isActive ? 'ds-sort-indicator--active' : ''}`}>
                        {isActive ? (sortDir === 'asc' ? '▲' : '▼') : '⇅'}
                      </span>
                    )}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {paginatedRows.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  style={{
                    textAlign: 'center',
                    padding: 'var(--space-8)',
                    color: 'var(--color-text-muted)',
                  }}
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              paginatedRows.map((row, idx) => (
                <tr key={row.key ?? idx}>
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className={numSet.has(col.key) ? 'ds-table--num' : ''}
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
      {pageSize > 0 && totalPages > 1 && (
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      )}
    </div>
  );
}
