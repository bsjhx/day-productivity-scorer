async function loadData() {
    const res = await fetch('/day/?from=2025-07-06&to=2026-07-09');
    const data = await res.json();
    console.log(data);
    document.getElementById('output').innerHTML = data.map(a => `<p>${a.date}: ${a.score}</p>`).join('');
}

async function getDays() {
    const from = '2026-07-06';
    const to = '2026-07-09';

    const url = `/day/?from=${from}&to=${to}`;

    const response = await fetch(url);

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    console.log(data);
    return data;
}