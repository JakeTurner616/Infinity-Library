## simple-libgen-desktop

A straightforward, reliable, and lightweight Libgen library client written in Java that doesn't require database dumps and avoids the common issues that plague Libgen Desktop. It enables direct downloading from the libgen.li and librrary.lol mirrors.

<div style="text-align: center;">
  <div style="display: inline-block;">
    <p>Using simple-libgen-desktop to search for books and mirrors:</p>
      <img src="https://raw.githubusercontent.com/JakeTurner616/simple-libgen-desktop/main/docs/libgendemo200percentspeed.gif" alt="simple-libgen-desktop demo">
  </div>

## Features

- Search and download books from libgen.li and library.lol.
- Displays the list of mirrors for any given book across annas-archive.org, library.lol or any other libgen.li supported mirror.
- Allows for filtering against any language and/or any media type.
- Fetches book details, covers, and direct download URLs across multiple mirrors all in one place.
- Directly download from the libgen.li or library.lol mirror to a selected local location.
- Multi-threaded downloading logic allows for downloading more than one book simultaneously without blocking the UI.
- Pagination allows for in-depth searching of results.
- Uses Java Swing (jswing) and Jsoup for graphics and web scraping, respectively.

## License

This project is licensed under the GNU GPL v3.0 License - see the [LICENSE](https://github.com/JakeTurner616/simple-libgen-desktop/blob/main/LICENSE) file for details.
