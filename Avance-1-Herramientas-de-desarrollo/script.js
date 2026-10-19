const DEFAULT_MENU = [
  {id:1,name:'Seco de cabrito',category:'Criollos',price:28,emoji:'🍛',desc:'Con frejoles y arroz blanco.',available:true},
  {id:2,name:'Arroz con pato',category:'Criollos',price:26,emoji:'🍚',desc:'Arroz verde, pato tierno y salsa criolla.',available:true},
  {id:3,name:'Ceviche clásico',category:'Marinos',price:30,emoji:'🐟',desc:'Pescado fresco, limón, cebolla, camote y choclo.',available:true},
  {id:4,name:'Sudado de pescado',category:'Marinos',price:27,emoji:'🍲',desc:'Pescado en caldo criollo con tomate y yuca.',available:false},
  {id:5,name:'Lomo saltado',category:'Criollos',price:29,emoji:'🥩',desc:'Carne salteada, papas fritas y arroz.',available:true},
  {id:6,name:'Ají de gallina',category:'Criollos',price:23,emoji:'🍗',desc:'Crema de ají amarillo, pollo, papa y arroz.',available:true},
  {id:7,name:'Chicha morada',category:'Bebidas',price:7,emoji:'🥤',desc:'Preparación tradicional de maíz morado.',available:true},
  {id:8,name:'Maracuyá',category:'Bebidas',price:8,emoji:'🧃',desc:'Refresco natural de maracuyá.',available:true},
  {id:9,name:'Mazamorra morada',category:'Postres',price:9,emoji:'🍮',desc:'Postre tradicional con frutas y especias.',available:true}
];

const LS = {menu:'sabor_menu',orders:'sabor_orders',reservations:'sabor_reservations',clients:'sabor_clients'};
let menu = load(LS.menu, DEFAULT_MENU);
let orders = load(LS.orders, []);
let reservations = load(LS.reservations, []);
let clients = load(LS.clients, []);
let cart = [];
let activeCategory = 'Todos';

function load(key,fallback){ try{return JSON.parse(localStorage.getItem(key)) ?? structuredClone(fallback)}catch{return structuredClone(fallback)} }
function save(key,data){localStorage.setItem(key,JSON.stringify(data))}
function money(n){return `S/ ${Number(n).toFixed(2)}`}
function uid(prefix){return `${prefix}-${Date.now().toString().slice(-6)}`}
function toast(msg){const el=document.getElementById('toast');el.textContent=msg;el.classList.add('show');clearTimeout(window.__toast);window.__toast=setTimeout(()=>el.classList.remove('show'),2600)}

function renderFilters(){
  const categories=['Todos',...new Set(menu.map(x=>x.category))];
  categoryFilters.innerHTML=categories.map(c=>`<button class="filter ${c===activeCategory?'active':''}" data-cat="${c}">${c}</button>`).join('');
  categoryFilters.querySelectorAll('button').forEach(b=>b.onclick=()=>{activeCategory=b.dataset.cat;renderFilters();renderMenu()});
}
function renderMenu(){
  const data=activeCategory==='Todos'?menu:menu.filter(x=>x.category===activeCategory);
  menuGrid.innerHTML=data.map(d=>`<article class="dish-card">
    <div class="dish-visual"><span>${d.emoji}</span><span class="badge ${d.available?'':'off'}">${d.available?'Disponible':'Agotado'}</span></div>
    <div><h3>${d.name}</h3><p>${d.desc}</p></div>
    <div class="dish-bottom"><strong>${money(d.price)}</strong><button class="add-btn" data-id="${d.id}" ${d.available?'':'disabled'}>+</button></div>
  </article>`).join('');
  menuGrid.querySelectorAll('.add-btn:not(:disabled)').forEach(btn=>btn.onclick=()=>addToCart(Number(btn.dataset.id)));
}
function addToCart(id){const dish=menu.find(x=>x.id===id);const item=cart.find(x=>x.id===id);if(item)item.qty++;else cart.push({...dish,qty:1});renderCart();toast(`${dish.name} agregado al pedido`)}
function changeQty(id,delta){const item=cart.find(x=>x.id===id);if(!item)return;item.qty+=delta;if(item.qty<=0)cart=cart.filter(x=>x.id!==id);renderCart()}
function renderCart(){
  const count=cart.reduce((s,x)=>s+x.qty,0); const total=cart.reduce((s,x)=>s+x.price*x.qty,0);
  cartCount.textContent=count; cartItemsLabel.textContent=`${count} ${count===1?'producto':'productos'}`; cartTotal.textContent=money(total);
  cartItems.innerHTML=cart.length?cart.map(x=>`<div class="cart-item"><div><strong>${x.name}</strong><br><small>${money(x.price)} c/u</small></div><div class="qty"><button data-id="${x.id}" data-d="-1">−</button><span>${x.qty}</span><button data-id="${x.id}" data-d="1">+</button></div><strong>${money(x.price*x.qty)}</strong></div>`).join(''):'<div class="empty">Aún no agregas productos.</div>';
  cartItems.querySelectorAll('button').forEach(b=>b.onclick=()=>changeQty(Number(b.dataset.id),Number(b.dataset.d)));
}
function upsertClient(name,phone){
  const clean=phone.trim();let c=clients.find(x=>x.phone===clean);if(c){c.name=name;c.visits=(c.visits||0)+1}else clients.unshift({id:uid('CLI'),name,phone:clean,visits:1});save(LS.clients,clients)
}

orderForm.addEventListener('submit',e=>{
  e.preventDefault(); if(!cart.length){toast('Agrega al menos un producto al pedido');return}
  const total=cart.reduce((s,x)=>s+x.price*x.qty,0); const name=orderName.value.trim(),phone=orderPhone.value.trim();
  orders.unshift({id:uid('PED'),date:new Date().toLocaleString('es-PE'),name,phone,type:orderType.value,notes:orderNotes.value.trim(),items:cart.map(x=>({name:x.name,qty:x.qty,price:x.price})),total,status:'Pendiente'});
  upsertClient(name,phone); save(LS.orders,orders); cart=[]; orderForm.reset(); renderCart(); renderAdmin(); toast('Pedido confirmado correctamente');
});

const times=['12:00','12:30','13:00','13:30','14:00','18:30','19:00','19:30','20:00','20:30','21:00'];
resTime.innerHTML=times.map(t=>`<option>${t}</option>`).join('');
resDate.min=new Date().toISOString().split('T')[0];
reservationForm.addEventListener('submit',e=>{
  e.preventDefault(); const name=resName.value.trim(),phone=resPhone.value.trim();
  reservations.unshift({id:uid('RES'),name,phone,date:resDate.value,time:resTime.value,guests:Number(resGuests.value),notes:resNotes.value.trim(),status:'Confirmada'});
  upsertClient(name,phone); save(LS.reservations,reservations); reservationForm.reset();resGuests.value=2;renderAdmin();toast('Reserva registrada correctamente');
});

function renderAdmin(){
  statOrders.textContent=orders.length;statReservations.textContent=reservations.length;statAvailable.textContent=menu.filter(x=>x.available).length;statSales.textContent=money(orders.reduce((s,x)=>s+x.total,0));
  adminOrders.innerHTML=orders.length?`<div class="table-wrap"><table class="data-table"><thead><tr><th>ID</th><th>Cliente</th><th>Detalle</th><th>Total</th><th>Estado</th><th>Acción</th></tr></thead><tbody>${orders.map(o=>`<tr><td>${o.id}<br><small>${o.date}</small></td><td>${o.name}<br><small>${o.phone}</small></td><td>${o.items.map(i=>`${i.qty}× ${i.name}`).join('<br>')}<br><small>${o.type}</small></td><td>${money(o.total)}</td><td><span class="status ${o.status==='Pendiente'?'pending':''}">${o.status}</span></td><td><button class="small-btn order-status" data-id="${o.id}">${o.status==='Pendiente'?'Marcar listo':'Reabrir'}</button></td></tr>`).join('')}</tbody></table></div>`:'<div class="empty">No hay pedidos registrados.</div>';
  adminOrders.querySelectorAll('.order-status').forEach(b=>b.onclick=()=>{const o=orders.find(x=>x.id===b.dataset.id);o.status=o.status==='Pendiente'?'Listo':'Pendiente';save(LS.orders,orders);renderAdmin()});

  adminReservations.innerHTML=reservations.length?`<div class="table-wrap"><table class="data-table"><thead><tr><th>ID</th><th>Cliente</th><th>Fecha</th><th>Hora</th><th>Comensales</th><th>Estado</th></tr></thead><tbody>${reservations.map(r=>`<tr><td>${r.id}</td><td>${r.name}<br><small>${r.phone}</small></td><td>${r.date}</td><td>${r.time}</td><td>${r.guests}</td><td><span class="status">${r.status}</span></td></tr>`).join('')}</tbody></table></div>`:'<div class="empty">No hay reservas registradas.</div>';

  adminMenu.innerHTML=`<div class="table-wrap"><table class="data-table"><thead><tr><th>Plato</th><th>Categoría</th><th>Precio</th><th>Disponibilidad</th><th>Acción</th></tr></thead><tbody>${menu.map(d=>`<tr><td>${d.emoji} ${d.name}</td><td>${d.category}</td><td>${money(d.price)}</td><td><span class="status ${d.available?'':'pending'}">${d.available?'Disponible':'Agotado'}</span></td><td><button class="small-btn toggle-menu" data-id="${d.id}">${d.available?'Marcar agotado':'Marcar disponible'}</button></td></tr>`).join('')}</tbody></table></div>`;
  adminMenu.querySelectorAll('.toggle-menu').forEach(b=>b.onclick=()=>{const d=menu.find(x=>x.id===Number(b.dataset.id));d.available=!d.available;save(LS.menu,menu);renderMenu();renderAdmin();toast('Disponibilidad actualizada')});

  adminClients.innerHTML=clients.length?`<div class="client-list">${clients.map(c=>`<article class="client-card"><strong>${c.name}</strong><br><small>${c.phone}</small><p>${c.visits} interacción(es) registrada(s)</p></article>`).join('')}</div>`:'<div class="empty">Aún no hay clientes registrados. Se crean al realizar pedidos o reservas.</div>';
}

document.querySelectorAll('.tab').forEach(tab=>tab.onclick=()=>{document.querySelectorAll('.tab,.tab-panel').forEach(x=>x.classList.remove('active'));tab.classList.add('active');document.getElementById(tab.dataset.tab).classList.add('active')});
menuToggle.onclick=()=>mainNav.classList.toggle('open');document.querySelectorAll('#mainNav a').forEach(a=>a.onclick=()=>mainNav.classList.remove('open'));
cartButton.onclick=()=>document.getElementById('pedidos').scrollIntoView({behavior:'smooth'});
resetDemo.onclick=()=>{if(confirm('¿Restablecer todos los datos de la demo?')){Object.values(LS).forEach(k=>localStorage.removeItem(k));menu=structuredClone(DEFAULT_MENU);orders=[];reservations=[];clients=[];cart=[];activeCategory='Todos';renderFilters();renderMenu();renderCart();renderAdmin();toast('Demo restablecida')}};

renderFilters();renderMenu();renderCart();renderAdmin();
