export default function Slide4() {
  return (
    <div className="w-screen h-screen overflow-hidden relative deck-frame">
      <div className="absolute left-0 top-0 h-[35vh] w-[24vw] rounded-br-[10vw] bg-[#b9df74]" />
      <div className="relative px-[7vw] pt-[7.5vh]">
        <h1 className="deck-title max-w-[80vw]" data-layout-content="title">Four tables, one game library</h1>
        <div className="mt-[3.5vh] grid grid-cols-[.94fr_1.06fr] items-center gap-[3vw]">
          <svg viewBox="0 0 820 620" className="h-[67vh] w-full" role="img" aria-label="Four-table relationship diagram for genres, games, users, and user_games">
            <path className="relation" d="M258 180 H390" />
            <path className="relation" d="M500 240 V370" />
            <path className="relation" d="M260 430 H390" />
            <rect className="table-node-highlight" x="60" y="120" width="200" height="120" rx="28" />
            <rect className="table-node" x="390" y="120" width="230" height="120" rx="28" />
            <rect className="table-node" x="60" y="370" width="200" height="120" rx="28" />
            <rect className="table-node-highlight" x="390" y="370" width="230" height="120" rx="28" />
            <text className="table-label" x="160" y="193" textAnchor="middle">genres</text>
            <text className="table-label" x="505" y="193" textAnchor="middle">games</text>
            <text className="table-label" x="160" y="443" textAnchor="middle">users</text>
            <text className="table-label" x="505" y="443" textAnchor="middle">user_games</text>
            <text className="relation-label" x="276" y="154">one genre → many games</text>
            <text className="relation-label" x="290" y="535">many-to-many</text>
          </svg>
          <ul className="deck-copy flex flex-col gap-[2.55vh]" data-layout-content="relationship details">
            <li>genres classifies games; one genre can have many games</li>
            <li>games stores title, link, publication date, and genre</li>
            <li>users stores player names and emails</li>
            <li>user_games connects users and games; stores rating and play count</li>
            <li>The junction table models the many-to-many relationship</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
