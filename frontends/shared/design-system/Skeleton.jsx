export default function Skeleton({ width, height = 16, className = '', style: styleProp, ...rest }) {
  const style = { ...styleProp };
  if (width) style.width = typeof width === 'number' ? `${width}px` : width;
  if (height) style.height = typeof height === 'number' ? `${height}px` : height;
  return <div className={`ds-skeleton ${className}`.trim()} style={style} aria-hidden="true" {...rest} />;
}

export function TableSkeleton({ rows = 5, columns = 4 }) {
  return (
    <div className="ds-table-wrap">
      <table className="ds-table">
        <thead>
          <tr>
            {Array.from({ length: columns }, (_, i) => (
              <th key={i}><Skeleton height={14} width="80%" /></th>
            ))}
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: rows }, (_, i) => (
            <tr key={i}>
              {Array.from({ length: columns }, (_, j) => (
                <td key={j}><Skeleton height={14} width={j === 0 ? '60%' : '90%'} /></td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
