import React from 'react';
export default function Panel({ title, action, children, className = '', id }) {
  return <section id={id} className={`panel ${className}`}><div className="panel-heading"><h2>{title}</h2>{action}</div>{children}</section>;
}
