$version: "2.0"

namespace ai.shop

use aws.protocols#restJson1

@title("SD Shop Service")
@restJson1
service Service {
    version: "2024-06-01",
    operations: [ListThemes],
}

structure ThemePreference {
    @required
    tags: StringList,
    description: String,
}

structure ThemeSummary {
    id: String,
    title: String,
    updatedAt: Timestamp,
    preference: ThemePreference,
}

list ThemeSummaryList {
    member: ThemeSummary,
}

@paginated(items: "items", pageToken: "nextToken")
operation ListThemes {
    input: ListThemesInput,
    output: ListThemesOutput,
}

structure ListThemesInput {
    nextToken: String,
    pageSize: Integer,
    updatedAfter: Timestamp,
}

structure ListThemesOutput {
    items: ThemeSummaryList,
    nextToken: String,
}
