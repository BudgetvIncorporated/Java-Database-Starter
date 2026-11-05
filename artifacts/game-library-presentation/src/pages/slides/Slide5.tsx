export default function Slide5() {
  return (
    <div className="w-screen h-screen overflow-hidden relative deck-frame">
      <div className="absolute bottom-0 right-0 h-[27vh] w-[36vw] rounded-tl-[12vw] bg-[#b9df74]" />
      <div className="relative px-[7vw] pt-[8vh]">
        <h1 className="deck-title max-w-[78vw]" data-layout-content="title">What the database answers</h1>
        <div className="mt-[6vh] grid grid-cols-[1.2fr_.8fr] gap-[5vw]">
          <ul className="deck-copy flex flex-col gap-[3.1vh]" data-layout-content="database examples">
            <li>Which players saved each game? JOIN user_games, games, and users</li>
            <li>How many games and plays does each user have? GROUP BY user</li>
            <li>The report uses COUNT for games, SUM for plays, and AVG for ratings</li>
            <li>User-provided values use JDBC PreparedStatements</li>
            <li>These SQL features are available in the running app</li>
          </ul>
          <div className="relative mt-[1vh] flex h-[57vh] flex-col justify-center overflow-hidden rounded-[2.5vw] bg-[#286747] px-[3vw] text-[#f2f6ec]">
            <div className="absolute right-[-4vw] top-[-8vh] h-[24vw] w-[24vw] rounded-full bg-[#b9df74]" />
            <p className="relative font-display text-[5.4vw] font-bold leading-[1] tracking-[-.06em]">JOIN</p>
            <p className="relative mt-[2.5vh] font-display text-[3vw] font-semibold">COUNT · SUM · AVG</p>
            <div className="relative mt-[4vh] h-[.9vh] w-[14vw] rounded-full bg-[#e5b94b]" />
          </div>
        </div>
      </div>
    </div>
  );
}
