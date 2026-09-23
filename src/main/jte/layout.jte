@param String title
@param gg.jte.Content content
@param gg.jte.Content head = null

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <%-- A browser that reads SVG icons takes the second link. The PNG is for one that does not. --%>
    <link rel="icon" href="/favicon-32.png" sizes="32x32" type="image/png">
    <link rel="icon" href="/favicon.svg" type="image/svg+xml">
    @if(head != null)${head}@endif
    <script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Baloo+2:wght@500;700;800&family=JetBrains+Mono:wght@400;700&display=swap">
    <style>
        :root { --cream: #FDF6E3; --ink: #171310; }
        body { background: var(--cream); color: var(--ink);
               font-family: 'Baloo 2', ui-rounded, system-ui, sans-serif; }
        .mono { font-family: 'JetBrains Mono', ui-monospace, monospace; }
        .card { border: 4px solid var(--ink); border-radius: 1.75rem; background: var(--cream);
                box-shadow: 8px 8px 0 0 var(--ink); }
        .tile { border: 4px solid var(--ink); border-radius: 1.5rem; box-shadow: 6px 6px 0 0 var(--ink); }
        .pill { border: 3px solid var(--ink); border-radius: 999px; font-weight: 800;
                letter-spacing: .04em; text-transform: uppercase; font-size: .8rem; }
        .btn-solid { background: var(--ink); color: var(--cream); border-radius: 999px; font-weight: 800; }
        .btn-hollow { border: 3px solid var(--ink); border-radius: 999px; font-weight: 800; }
    </style>
</head>
<body class="p-6 md:p-10">
<main class="mx-auto max-w-4xl">
    ${content}
</main>
</body>
</html>
