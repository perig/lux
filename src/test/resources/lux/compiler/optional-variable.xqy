import module namespace fact="http://luxdb.org/factorial" at "factorial.xqy";

let $x as xs:string? := ("a", "b", "c")[fact:factorial(2)]
let $y as xs:string? := ("x", $x)[fact:factorial(3)]
return concat($x, $y)
