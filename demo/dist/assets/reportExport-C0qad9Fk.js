function b(r,o,a="report.csv"){const l=o.map(t=>t.label||t.key).join(","),i=t=>{const e=String(t??"");return e.includes(",")||e.includes('"')||e.includes(`
`)?`"${e.replace(/"/g,'""')}"`:e},c=[l];for(const t of r){const e=o.map(d=>i(t[d.key]));c.push(e.join(","))}const u="\uFEFF"+c.join(`
`),p=new Blob([u],{type:"text/csv;charset=utf-8"}),s=URL.createObjectURL(p),n=document.createElement("a");n.href=s,n.download=a,n.click(),URL.revokeObjectURL(s)}export{b as e};
