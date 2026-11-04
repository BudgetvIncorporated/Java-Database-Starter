export default function Slide6() {
  return (
    <div className="w-screen h-screen overflow-hidden relative lime-surface">
      <div className="absolute right-[-4vw] top-[-16vh] h-[45vw] w-[45vw] rounded-full border-[2vw] border-[#286747]/15" />
      <div className="relative px-[7vw] pt-[8vh]">
        <h1 className="deck-title max-w-[77vw]">Safe input and Java algorithm</h1>
        <div className="mt-[6vh] grid grid-cols-[.9fr_1.1fr] gap-[5vw]">
          <div className="flex flex-col justify-between border-r-[.2vw] border-[#286747]/30 pr-[4vw]">
            <p className="font-display text-[6vw] font-bold leading-[.95] tracking-[-.06em]">Java</p>
            <p className="font-display text-[2.7vw] font-semibold leading-[1.08]">insertion sort</p>
            <div className="flex items-end gap-[.9vw]">
              <div className="h-[7vh] w-[1.3vw] rounded-t-full bg-[#286747]" />
              <div className="h-[13vh] w-[1.3vw] rounded-t-full bg-[#286747]" />
              <div className="h-[10vh] w-[1.3vw] rounded-t-full bg-[#286747]" />
              <div className="h-[19vh] w-[1.3vw] rounded-t-full bg-[#286747]" />
              <div className="h-[15vh] w-[1.3vw] rounded-t-full bg-[#e5b94b]" />
            </div>
          </div>
          <ul className="deck-copy flex flex-col gap-[3.2vh]">
            <li>Validate non-empty titles, email addresses, web links, and YYYY-MM-DD dates</li>
            <li>Database rules protect unique names, valid ratings, and non-negative play counts</li>
            <li>SQL stays parameterized through PreparedStatements</li>
            <li>Java insertion sort orders games by title</li>
            <li>Tests cover DAO operations, reports, and input validation</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
