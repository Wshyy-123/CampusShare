export function resourceIdentity(value: {
  location: string;
  resourceType: string;
  unitLabel: string;
}): string {
  return JSON.stringify(
    [value.location, value.resourceType, value.unitLabel].map((field) =>
      field
        .normalize("NFKC")
        .replace(/[\s\p{Z}]+/gu, " ")
        .trim()
        .toLowerCase(),
    ),
  );
}
